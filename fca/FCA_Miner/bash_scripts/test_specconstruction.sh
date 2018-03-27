#!/bin/bash

echo 'parsing parameters'
PARAMS=()
NB_JOBS=3

while (( "$#" )); do
  case "$1" in
    -r|--search_repository)
      REP=$2
      if [[ -z "$REP" ]] ; then rep='.'; else rep=$REP; fi
      files=($(find $rep -name '*cadets-*cdm17.bin' -or -name '*trace-*cdm17.bin' -or -name '*five*cdm17.bin'))
      shift 2
      ;;
    -p|--port)
		len="$#"
		 if [[  ($2 == "-nr") || ($2 == "--number_rules") || ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-n") || ($2 == "--context_names") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   PORT+=(8080)
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-nr") || ($a == "--number_rules") || ($a == "-t") || ($a == "--rule_thresholds") || ($a == "-rn") || ($a == "--rule_names") || ($a == "-n") || ($a == "--context_names") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								PORT+=($a)
							fi			
			done
			s=${#PORT[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -N|--no_ingest)
      INGEST=0
      shift 1
      ;;
    -seq|--sequential_ingest)
      SEQ=1
      shift 1
      ;;
    -d|--context_directory)
      CONTEXT_DIR=$2
      shift 2
      ;;
    -m|--mem)
      MEM=$2
      shift 2
      ;;
    -w|--fca_workflow)
      FCA_WORKFLOW=$2
      shift 2
      ;;
    -ms|--fca_minsupp)
      		len="$#"
		 if [[  ($2 == "-nr") || ($2 == "--number_rules") || ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-n") || ($2 == "--context_names") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-p") || ($2 == "--port") || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   FCA_MINSUPP+=(0)
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-nr") || ($a == "--number_rules") || ($a == "-t") || ($a == "--rule_thresholds") || ($a == "-rn") || ($a == "--rule_names") || ($a == "-n") || ($a == "--context_names") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-p") || ($a == "--port") || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								FCA_MINSUPP+=($a)
							fi			
			done
			s=${#FCA_MINSUPP[@]}
			#echo $s
			shift "$s"
	fi
      ;;
    -n|--context_names)
		len="$#"
		 if [[  ($2 == "-nr") || ($2 == "--number_rules") || ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-p") || ($2 == "--port") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-n" ) || ($2 == "--context_names" ) || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   CONTEXT_NAMES+=( "ProcessEvent" )
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[  ($a == "-nr") || ($a == "--number_rules") || ($a == "-t") || ($a == "--rule_thresholds") || ($a == "-rn") || ($a == "--rule_names") || ($a == "-p") || ($a == "--port") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-n" ) || ($a == "--context_names" ) || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								CONTEXT_NAMES+=($a)
							fi			
			done
			s=${#CONTEXT_NAMES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -rs|--fca_rule_spec_dir)
      FCA_RULE_SPEC_DIR=$2
      shift 2
      ;;
    -rn|--rule_names)
		len="$#"
		 if [[  ($2 == "-nr") || ($2 == "--number_rules") || ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-p") || ($2 == "--port") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-n" ) || ($2 == "--context_names" ) || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   RULE_NAMES+=( "implication" )
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[ ($a == "-nr") || ($a == "--number_rules") || ($a == "-t") || ($a == "--rule_thresholds") || ($a == "-p") || ($a == "--port") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-n" ) || ($a == "--context_names" ) || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								RULE_NAMES+=($a)
							fi			
			done
			s=${#RULE_NAMES[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -t|--rule_thresholds)
		len="$#"
		 if [[  ($2 == "-nr") || ($2 == "--number_rules") || ($2 == "-n") || ($a == "--context_names") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-p") || ($2 == "--port") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   RULE_THRESHOLDS+=( 0.95 )
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
						if [[  ($a == "-nr") || ($a == "--number_rules") || ($a == "-n") || ($a == "--context_names") || ($a == "-rn") || ($a == "--rule_names") || ($a == "-p") || ($a == "--port") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								RULE_THRESHOLDS+=($a)
							fi			
			done
			s=${#RULE_THRESHOLDS[@]}
			#echo $s
			shift "$s"
	fi
      ;; 
    -nr|--number_rules)
		len="$#"
		 if [[  ($2 == "-t") || ($2 == "--rule_thresholds") || ($2 == "-rn") || ($2 == "--rule_names") || ($2 == "-p") || ($2 == "--port") || ($2 == "-r") || ($2 == "--search_repository") || ($2 == "-N") || ($2 == "--no_ingest") || ($2 == "-seq") || ($2 == "--sequential_ingest") || ($2 == "-m" ) || ($2 == "--mem" ) || ($2 == "-d") || ($2 == "--context_directory") || ($2 == "-w") || ($2 == "--fca_workflow") || ($2 == "-ms") || ($2 == "--fca_minsupp") || ($2 == "-n" ) || ($2 == "--context_names" ) || ($2 == "-rs") || ($2 == "--fca_rule_spec_dir" ) || ($2 == "-cd") || ($2 == "--csv_dir" ) || ($2 == "-oa") || ($2 == "--fca_analysis_output_dir" ) ]]
			then
		   NUMBER_RULES+=( "'*'" )
		   shift 1
		else
			for k in `seq 2 $len`
				do  
					a=${!k}
					#echo $a
						if [[ ($a == "-t") || ($a == "--rule_thresholds") || ($a == "-rn") || ($a == "--rule_names") || ($a == "-p") || ($a == "--port") || ($a == "-r") || ($a == "--search_repository") || ($a == "-N") || ($a == "--no_ingest") || ($a == "-seq") || ($a == "--sequential_ingest") || ($a == "-m" ) || ($a == "--mem" ) || ($a == "-d") || ($a == "--context_directory") || ($a == "-w") || ($a == "--fca_workflow") || ($a == "-ms") || ($a == "--fca_minsupp") || ($a == "-n" ) || ($a == "--context_names" ) || ($a == "-rs") || ($a == "--fca_rule_spec_dir" ) || ($a == "-cd") || ($a == "--csv_dir" ) || ($a == "-oa") || ($a == "--fca_analysis_output_dir" ) ]]
							then 
								#echo 'k' $k 'a' ${!k} 'condition!!!'
								break
							else 
								#echo 'k' $k 'a' ${!k}
								NUMBER_RULES+=($a)
							fi			
			done
			s=${#NUMBER_RULES[@]}
			#echo $numr
			shift "$s"
	fi
      ;; 
    -cd|--csv_dir)
      CSV_DIR=$2
      shift 2
      ;;
    -oa|--fca_analysis_output_dir)
      FCA_ANALYSIS_DIR=$2
      shift 2
      ;;
    --) # end argument parsing./fca/ingest_script.sh -p 8629 -m 4000 -db cadets_bovia -c n=ProcessEvent d=./fca/contextSpecFiles cp=./fca/csvContexts/cadets_bovia_ProcessEvent_1.csv p=8629 -fp w=both i=./fca/csvContexts/cadets_bovia_ProcessEvent_1.csv m=0.05 rs=./fca/rulesSpecs/rules_positive_implication.json oa=./fca/fcaAnalysis/cadets_bovia_ProcessEvent_1.txt
      shift
      break
      ;;
    -*|--*=) # unsupported flags
      echo "Error: Unsupported flag $1" >&2
      exit 1
      ;;
    *) # preserve positional arguments
      #PARAM="$PARAMS $1"
      PARAM+=($1)
      shift
      ;;
  esac
done

# set positional arguments in their proper place
eval set -- "$PARAMS"
#echo 'PARAM' $PARAM
echo 'parameters parsed'
#mem=${PARAM[0]}
#port=${PARAM[1]}
#dbkeyspace=${PARAM[2]}

#if [[ -z "$PORT" ]] ; then port=8080; else port=$PORT; fi
if [[ -z "$MEM" ]] ; then mem=5000; else mem=$MEM; fi
if [[ -z "$INGEST" ]] ; then ingest=1; else ingest=$INGEST; fi
if [[ -z "$SEQ" ]] ; then seq=0; else seq=$SEQ; fi
if [[ -z "$FCA_WORKFLOW" ]] ; then workflow="both"; else workflow=$FCA_WORKFLOW; fi
if [[ -z "$FCA_MINSUPP" ]] ; then minsupp+=(0); else minsupp=("${FCA_MINSUPP[@]}"); fi
if [[ -z "$CONTEXT_DIR" ]] ; then context_dir='./fca/contextSpecFiles'; else context_dir=$CONTEXT_DIR; fi
if [[ -z "$FCA_RULE_SPEC_DIR" ]] ; then rule_spec='./fca/rulesSpecs'; else rule_spec=$FCA_RULE_SPEC_DIR; fi
if [[ -z "$FCA_ANALYSIS_DIR" ]] ; then fca_analysis_dir='./fca/fcaAnalysis'; else fca_analysis_dir=$FCA_ANALYSIS_DIR; fi
if [[ -z "$CSV_DIR" ]] ; then csv_dir='./fca/csvContexts'; else csv_dir=$CSV_DIR; fi
if [[ -z "$CONTEXT_NAMES" ]] ; then context_names+=( 'ProcessEvent' ); else context_names=("${CONTEXT_NAMES[@]}"); fi
if [[ -z "$RULE_NAMES" ]] ; then rule_names+=( 'implication' ); elif [[ ${#RULE_NAMES[@]} -eq 1 ]] ; then rule_names+=(${RULE_NAMES[0]}) ; else rule_names=("${RULE_NAMES[@]}"); fi
if [[ -z "$RULE_THRESHOLDS" ]] ; then rule_thresholds+=( 0.95 ); elif [[ ${RULE_THRESHOLDS[@]} -eq 1 ]] ; then rule_thresholds+=(${RULE_THRESHOLDS[0]}) ; else rule_thresholds=("${RULE_THRESHOLDS[@]}"); fi
#if [[ -z "$INTERV" ]] ; then interval=10; else interval=$interv; fi
if [[ -z "$NUMBER_RULES" ]] ; then number_rules+=( "'*'" ); elif [[ ${#NUMBER_RULES[@]} -eq 1 ]] ; then number_rules+=(${NUMBER_RULES[0]}) ; else number_rules=("${NUMBER_RULES[@]}"); fi
if [[ ${#PORT[@]} -eq 0 ]] ; then port+=(8080); elif [[ ${#PORT[@]} -eq 1 ]] ; then port+=(${PORT[0]}) ; else port+=("${PORT[@]}") ; fi


if [[ ${#PORT[@]} -le 1 ]];
 then
	let "end_port=$port+2000"
	ports=($(shuf -i $port-$end_port -n ${#files[@]}))
 else
	ports=${port[@]}
fi
#construct rule spec files
echo 'Constructing rule specification files'
thresh_string='_thresh'
numrules_string='_numrules'
json_ext='.json'

inputargs=$(parallel echo --rule_properties {1} {2} {3} $rule_spec'/'{1}'_thresh'{2}'_numrules'{3}'.json' ::: ${rule_names[@]} ::: ${rule_thresholds[@]} ::: ${number_rules[@]} | sed " s/\(\.\{1\}\)\([0-9]\+\)\(_\)/_\2\3/g ; s/'//g; s/\*/all/g ; s/ all / '*' /g")
echo 'inputargs' $inputargs

rule_spec_files=( $(parallel echo --rule_properties {1} {2} {3} $rule_spec'/'{1}'_thresh'{2}'_numrules'{3}'.json' ::: ${rule_names[@]} ::: ${rule_thresholds[@]} ::: ${number_rules[@]} | awk '{print $NF}' | sed " s/\(\.\{1\}\)\([0-9]\+\)\(_\)/_\2\3/g ; s/'//g; s/\*/all/g ; s/ all / '*' /g") )
fileindices=${!rule_spec_files[*]}
for i in $fileindices
	do
	 echo 'rule_spec_files['$i']' ${rule_spec_files[i]}
done

pythonSpecGen_arg=( python3 fca/ruleSpecificationGeneration.py $inputargs )
#echo "${pythonSpecGen_arg[@]}"
eval "${pythonSpecGen_arg[@]}"
