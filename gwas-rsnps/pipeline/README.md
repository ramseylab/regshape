## Prototype Pipeline ##

# Requirements
- mysql client (for connecting to UCSC remote, HMDB local)
- mysql-server serving HMDB on root@localhost
- Python + {pandas, numpy, scipy, scikit-learn, matplotlib}
- iPython Notebook for frontend

# Features
- Final SQL to extract HGMD rSNPs; OR use a pre-populated list?
- SQL to extract control SNPs
-- downsample (select 3 nearest) to balance classes
- Generalize script for getting annotation data {rSNPs, control-SNPs}
- Pandas load train-test data
- Build a simple RF pipeline
- Evaluation

# Set up Virtualenv
```
(Current Path: PROJECT_HOME/workspace)
virtualenv venv
source workspace/venv/bin/activate 
pip install pandas numpy scipy scikit-learn matplotlib
```
# Setup + Start iPython 
```
(Current Path: PROJECT_HOME)
source workspace/venv/bin/activate # if not active 
pip install --upgrade ipython[notebook] # first time
ipython notebook # start ipynb server (opens browser tab)
```
